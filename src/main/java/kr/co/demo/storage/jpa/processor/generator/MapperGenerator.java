package kr.co.demo.storage.jpa.processor.generator;

import com.squareup.javapoet.*;
import kr.co.demo.storage.jpa.processor.util.NamingUtils;
import kr.co.demo.core.storage.annotation.StorageRelation;
import kr.co.demo.core.storage.annotation.StorageTransient;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DomainMapper 구현 클래스 생성기
 *
 * <p>도메인 객체와 JPA Entity 간의 변환을 담당하는
 * DomainMapper 구현체를 자동 생성합니다.
 *
 * <h2>생성 규칙</h2>
 * <ul>
 *     <li>클래스명: {@code {도메인명}StorageMapper}</li>
 *     <li>{@code @Component} 어노테이션 자동 추가 (Spring Bean 등록)</li>
 *     <li>{@code @StorageTransient} 필드는 매핑에서 제외</li>
 *     <li>{@code @StorageRelation} 필드는 매핑에서 제외 (연관관계는 별도 처리 필요)</li>
 * </ul>
 *
 * <h2>생성 예시</h2>
 * <pre>{@code
 * // 생성된 OrderStorageMapper.java
 * @Component
 * public class OrderStorageMapper implements DomainMapper<Order, OrderEntity> {
 *
 *     @Override
 *     public OrderEntity toStorage(Order domain) {
 *         if (domain == null) return null;
 *         OrderEntity entity = new OrderEntity();
 *         entity.setId(domain.getId());
 *         entity.setOrderNumber(domain.getOrderNumber());
 *         return entity;
 *     }
 *
 *     @Override
 *     public Order toDomain(OrderEntity entity) {
 *         if (entity == null) return null;
 *         Order domain = new Order();
 *         domain.setId(entity.getId());
 *         domain.setOrderNumber(entity.getOrderNumber());
 *         return domain;
 *     }
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
public class MapperGenerator {

	/** 소스 파일 생성을 위한 Filer */
	private final Filer filer;

	/**
	 * MapperGenerator 생성자
	 *
	 * @param filer 소스 파일 생성을 위한 Filer
	 */
	public MapperGenerator(Filer filer) {
		this.filer = filer;
	}

	/**
	 * 도메인 클래스로부터 DomainMapper 구현체를 생성합니다.
	 *
	 * <p>생성되는 파일 위치: {@code {packageName}.mapper.{ClassName}StorageMapper.java}
	 *
	 * @param domainClass 도메인 클래스의 TypeElement
	 * @param packageName 도메인 클래스의 패키지명
	 * @throws IOException 파일 생성 실패 시
	 */
	public void generate(TypeElement domainClass, String packageName) throws IOException {
		String domainClassName = domainClass.getSimpleName().toString();
		String entityClassName = domainClassName + "Entity";
		String mapperClassName = domainClassName + "StorageMapper";

		ClassName domainType = ClassName.get(packageName, domainClassName);
		ClassName entityType = ClassName.get(packageName + ".entity", entityClassName);
		ClassName domainMapperInterface = ClassName.get("kr.co.demo.mapper", "DomainMapper");

		// 매핑 대상 필드 수집 (StorageTransient, StorageRelation 제외)
		List<VariableElement> fields = collectMappableFields(domainClass);

		// toStorage 메서드 생성
		MethodSpec toStorageMethod = generateToStorageMethod(domainType, entityType, fields);

		// toDomain 메서드 생성
		MethodSpec toDomainMethod = generateToDomainMethod(domainType, entityType, fields);

		// Mapper 클래스 생성
		TypeSpec mapperClass = TypeSpec.classBuilder(mapperClassName)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(ClassName.get("org.springframework.stereotype", "Component"))
				.addSuperinterface(ParameterizedTypeName.get(domainMapperInterface, domainType, entityType))
				.addMethod(toStorageMethod)
				.addMethod(toDomainMethod)
				.build();

		JavaFile javaFile = JavaFile.builder(packageName + ".mapper", mapperClass).build();
		javaFile.writeTo(filer);
	}

	/**
	 * 매핑 대상 필드를 수집합니다.
	 *
	 * <p>다음 필드들은 매핑에서 제외됩니다:
	 * <ul>
	 *     <li>{@code @StorageTransient}가 붙은 필드</li>
	 *     <li>{@code @StorageRelation}이 붙은 필드 (연관관계는 별도 처리 필요)</li>
	 * </ul>
	 *
	 * @param domainClass 도메인 클래스의 TypeElement
	 * @return 매핑 대상 필드 목록
	 */
	private List<VariableElement> collectMappableFields(TypeElement domainClass) {
		List<VariableElement> fields = new ArrayList<>();

		for (Element enclosed : domainClass.getEnclosedElements()) {
			if (enclosed.getKind() != ElementKind.FIELD) continue;

			VariableElement field = (VariableElement) enclosed;

			// @StorageTransient 제외
			if (field.getAnnotation(StorageTransient.class) != null) continue;

			// @StorageRelation 제외 (연관관계는 별도 처리 필요)
			if (field.getAnnotation(StorageRelation.class) != null) continue;

			fields.add(field);
		}

		return fields;
	}

	/**
	 * toStorage 메서드를 생성합니다.
	 *
	 * <p>Domain → Entity 변환 로직을 생성합니다.
	 * null 체크 후 각 필드의 getter/setter를 호출하는 코드를 생성합니다.
	 *
	 * @param domainType 도메인 타입
	 * @param entityType Entity 타입
	 * @param fields     매핑 대상 필드 목록
	 * @return 생성된 toStorage MethodSpec
	 */
	private MethodSpec generateToStorageMethod(ClassName domainType, ClassName entityType,
	                                           List<VariableElement> fields) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("toStorage")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(domainType, "domain")
				.returns(entityType)
				.beginControlFlow("if (domain == null)")
				.addStatement("return null")
				.endControlFlow()
				.addStatement("$T entity = new $T()", entityType, entityType);

		// 각 필드에 대해 entity.setXxx(domain.getXxx()) 생성
		for (VariableElement field : fields) {
			String fieldName = field.getSimpleName().toString();
			String getter = "get" + NamingUtils.capitalize(fieldName);
			String setter = "set" + NamingUtils.capitalize(fieldName);
			builder.addStatement("entity.$N(domain.$N())", setter, getter);
		}

		builder.addStatement("return entity");
		return builder.build();
	}

	/**
	 * toDomain 메서드를 생성합니다.
	 *
	 * <p>Entity → Domain 변환 로직을 생성합니다.
	 * null 체크 후 각 필드의 getter/setter를 호출하는 코드를 생성합니다.
	 *
	 * @param domainType 도메인 타입
	 * @param entityType Entity 타입
	 * @param fields     매핑 대상 필드 목록
	 * @return 생성된 toDomain MethodSpec
	 */
	private MethodSpec generateToDomainMethod(ClassName domainType, ClassName entityType,
	                                          List<VariableElement> fields) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("toDomain")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(entityType, "entity")
				.returns(domainType)
				.beginControlFlow("if (entity == null)")
				.addStatement("return null")
				.endControlFlow()
				.addStatement("$T domain = new $T()", domainType, domainType);

		// 각 필드에 대해 domain.setXxx(entity.getXxx()) 생성
		for (VariableElement field : fields) {
			String fieldName = field.getSimpleName().toString();
			String getter = "get" + NamingUtils.capitalize(fieldName);
			String setter = "set" + NamingUtils.capitalize(fieldName);
			builder.addStatement("domain.$N(entity.$N())", setter, getter);
		}

		builder.addStatement("return domain");
		return builder.build();
	}
}
