package kr.co.demo.processor;

import com.google.auto.service.AutoService;
import kr.co.demo.processor.generator.EntityGenerator;
import kr.co.demo.processor.generator.MapperGenerator;
import kr.co.demo.processor.generator.RepositoryGenerator;
import kr.co.demo.storage.annotation.StorageTable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

/**
 * {@link StorageTable} 어노테이션을 처리하는 Annotation Processor
 *
 * <p>컴파일 타임에 도메인 객체를 분석하여 다음 클래스들을 자동 생성합니다:
 * <ul>
 *     <li>JPA Entity 클래스 ({@code *Entity.java})</li>
 *     <li>DomainMapper 구현체 ({@code *StorageMapper.java})</li>
 *     <li>JpaRepository 인터페이스 ({@code *EntityRepository.java})</li>
 * </ul>
 *
 * <p>생성된 파일 위치:
 * <pre>
 * build/generated/sources/annotationProcessor/java/main/
 * └── {패키지}/
 *     ├── entity/
 *     │   └── OrderEntity.java
 *     ├── mapper/
 *     │   └── OrderStorageMapper.java
 *     └── repository/
 *         └── OrderEntityRepository.java
 * </pre>
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 도메인 객체 정의
 * @StorageTable("orders")
 * public class Order {
 *     @StorageId
 *     private Long id;
 *
 *     @StorageColumn(nullable = false)
 *     private String orderNumber;
 * }
 *
 * // 빌드 시 자동 생성됨:
 * // - OrderEntity.java
 * // - OrderStorageMapper.java
 * // - OrderEntityRepository.java
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 * @see StorageTable
 * @see EntityGenerator
 * @see MapperGenerator
 * @see RepositoryGenerator
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("kr.co.demo.storage.annotation.StorageTable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class StorageJpaProcessor extends AbstractProcessor {

	/** 소스 파일 생성을 위한 Filer */
	private Filer filer;

	/** 컴파일 메시지 출력을 위한 Messager */
	private Messager messager;

	/** Element 유틸리티 */
	private Elements elementUtils;

	/** JPA Entity 생성기 */
	private EntityGenerator entityGenerator;

	/** DomainMapper 생성기 */
	private MapperGenerator mapperGenerator;

	/** JpaRepository 생성기 */
	private RepositoryGenerator repositoryGenerator;

	/**
	 * Processor 초기화
	 *
	 * <p>ProcessingEnvironment로부터 필요한 유틸리티들을 획득하고,
	 * 각 Generator 인스턴스를 생성합니다.
	 *
	 * @param processingEnv 컴파일러가 제공하는 처리 환경
	 */
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.filer = processingEnv.getFiler();
		this.messager = processingEnv.getMessager();
		this.elementUtils = processingEnv.getElementUtils();

		this.entityGenerator = new EntityGenerator(filer);
		this.mapperGenerator = new MapperGenerator(filer);
		this.repositoryGenerator = new RepositoryGenerator(filer);
	}

	/**
	 * 어노테이션 처리 수행
	 *
	 * <p>{@link StorageTable}이 붙은 모든 클래스를 순회하며
	 * Entity, Mapper, Repository를 생성합니다.
	 *
	 * @param annotations 처리할 어노테이션 타입 집합
	 * @param roundEnv    현재 라운드의 환경 정보
	 * @return 어노테이션이 이 Processor에 의해 처리되었으면 true
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getElementsAnnotatedWith(StorageTable.class)) {
			// 클래스가 아닌 곳에 사용된 경우 에러
			if (element.getKind() != ElementKind.CLASS) {
				error("@StorageTable은 클래스에만 사용 가능합니다", element);
				continue;
			}

			TypeElement domainClass = (TypeElement) element;
			String packageName = getPackageName(domainClass);

			try {
				// 1. Entity 생성
				entityGenerator.generate(domainClass, packageName);
				note("Generated Entity for " + domainClass.getSimpleName());

				// 2. Mapper 생성
				mapperGenerator.generate(domainClass, packageName);
				note("Generated Mapper for " + domainClass.getSimpleName());

				// 3. Repository 생성
				repositoryGenerator.generate(domainClass, packageName);
				note("Generated Repository for " + domainClass.getSimpleName());

			} catch (IOException e) {
				error("코드 생성 실패: " + e.getMessage(), element);
			}
		}

		return true;
	}

	/**
	 * Element가 속한 패키지명을 반환합니다.
	 *
	 * @param element 대상 Element
	 * @return 패키지명 (예: "com.example.domain")
	 */
	private String getPackageName(TypeElement element) {
		return elementUtils.getPackageOf(element).getQualifiedName().toString();
	}

	/**
	 * 컴파일 에러 메시지를 출력합니다.
	 *
	 * @param message 에러 메시지
	 * @param element 에러가 발생한 Element
	 */
	private void error(String message, Element element) {
		messager.printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	/**
	 * 정보성 메시지를 출력합니다.
	 *
	 * @param message 출력할 메시지
	 */
	private void note(String message) {
		messager.printMessage(Diagnostic.Kind.NOTE, message);
	}
}
