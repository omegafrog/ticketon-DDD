package org.codenbug.app.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 프로젝트 루트 디렉토리 찾기
            Path projectRoot = findProjectRoot();
            Path staticDir = projectRoot.resolve("static");

            // static 디렉토리가 없으면 생성
            Files.createDirectories(staticDir);

            // static/** 경로를 프로젝트 루트의 static 폴더로 매핑
            String staticLocation =
                    "file:" + staticDir.toAbsolutePath().toString().replace("\\", "/") + "/";

            System.out.println("Static resource location: " + staticLocation);

            registry.addResourceHandler("/static/**").addResourceLocations(staticLocation)
                    .setCachePeriod(31536000) // 1년 캐시
                    .resourceChain(false);

        } catch (IOException e) {
            // 실패 시 기본 설정 사용
            registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        }
    }

    private Path findProjectRoot() throws IOException {
        // 현재 실행 위치에서 시작하여 상위 디렉토리로 이동하면서 프로젝트 루트 찾기
        Path currentPath = Paths.get("").toAbsolutePath();

        while (currentPath != null) {
            // 프로젝트 루트 표시 파일들 확인
            if (Files.exists(currentPath.resolve("build.gradle"))
                    || Files.exists(currentPath.resolve("settings.gradle"))
                    || Files.exists(currentPath.resolve("gradlew"))
                    || Files.exists(currentPath.resolve("pom.xml"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }

        // 프로젝트 루트를 찾지 못한 경우, 현재 작업 디렉토리 사용
        return Paths.get("").toAbsolutePath();
    }
}
