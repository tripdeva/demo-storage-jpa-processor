package kr.co.demo.processor.generator;

import com.squareup.javapoet.*;
import kr.co.demo.storage.annotation.StorageId;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import java.io.IOException;

/**
 * JpaRepository 인터페이스 생성기
 *
 * <p>도메인 객체에 대응하는 JpaRepository 인터페이스를 자동 생성합니다.
 *
 * <h2>생성 규칙</h2>
 * <ul>
 *     <li>인터페이스명: {@code {도메인명}EntityRepository}</li>
 *     <li>{@code JpaRepository<{Entity}, {ID타입}>}을 상속</li>
 *     <li>ID 타입은 {@code @StorageId}가 붙은 필드의 타입 사용</li>
 *     <li>ID 타입 미발견 시 기본값 {@code Long} 사용</li>
 * </ul>
 *
 * <h2>생성 예시</h2>
 * <pre>{@code
 * // 생성된 OrderEntityRepository.java
 * public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
public class RepositoryGenerator {

	/** 소스 파일 생성을 위한 Filer */
	private final Filer filer;

	/**
	 * RepositoryGenerator 생성자
	 *
	 * @param filer 소스 파일 생성을 위한 Filer
	 */
	public RepositoryGenerator(Filer filer) {
		this.filer = filer;
	}

	/**
	 * 도메인 클래스로부터 JpaRepository 인터페이스를 생성합니다.
	 *
	 * <p>생성되는 파일 위치: {@code {packageName}.repository.{ClassName}EntityRepository.java}
	 *
	 * @param domainClass 도메인 클래스의 TypeElement
	 * @param packageName 도메인 클래스의 패키지명
	 * @throws IOException 파일 생성 실패 시
	 */
	public void generate(TypeElement domainClass, String packageName) throws IOException {
		String domainClassName = domainClass.getSimpleName().toString();
		String entityClassName = domainClassName + "Entity";
		String repositoryName = domainClassName + "EntityRepository";

		ClassName entityType = ClassName.get(packageName + ".entity", entityClassName);
		ClassName jpaRepository = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");

		// @StorageId가 붙은 필드의 타입을 ID 타입으로 사용
		TypeName idType = findIdType(domainClass);

		// Repository 인터페이스 생성
		TypeSpec repositoryInterface = TypeSpec.interfaceBuilder(repositoryName)
				.addModifiers(Modifier.PUBLIC)
				.addSuperinterface(ParameterizedTypeName.get(jpaRepository, entityType, idType))
				.build();

		JavaFile javaFile = JavaFile.builder(packageName + ".repository", repositoryInterface).build();
		javaFile.writeTo(filer);
	}

	/**
	 * 도메인 클래스에서 ID 필드의 타입을 찾습니다.
	 *
	 * <p>{@code @StorageId}가 붙은 필드의 타입을 반환합니다.
	 * 찾지 못한 경우 기본값으로 {@code Long}을 반환합니다.
	 *
	 * @param domainClass 도메인 클래스의 TypeElement
	 * @return ID 필드의 타입 (박싱된 타입)
	 */
	private TypeName findIdType(TypeElement domainClass) {
		for (Element enclosed : domainClass.getEnclosedElements()) {
			if (enclosed.getKind() != ElementKind.FIELD) continue;

			VariableElement field = (VariableElement) enclosed;
			if (field.getAnnotation(StorageId.class) != null) {
				// primitive 타입은 박싱 (long → Long)
				return TypeName.get(field.asType()).box();
			}
		}

		// 기본값: Long
		return ClassName.get(Long.class);
	}
}
