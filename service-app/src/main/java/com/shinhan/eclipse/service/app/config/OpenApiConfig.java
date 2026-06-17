package com.shinhan.eclipse.service.app.config;

import com.shinhan.eclipse.common.resolver.UserHeader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eclipseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eclipse Service API")
                        .description("신한 그룹 연계 해외 공모주 IPO 청약 앱 — 증권 도메인 API")
                        .version("v1"));
    }

    /** @UserHeader 파라미터가 있는 오퍼레이션에 X-User-Id 헤더 입력 필드를 추가 */
    @Bean
    public OperationCustomizer userHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean hasUserHeader = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(UserHeader.class));
            if (hasUserHeader) {
                operation.addParametersItem(
                        new HeaderParameter()
                                .name("X-User-Id")
                                .description("테스트 유저 ID (픽스처: 1)")
                                .required(true)
                                .schema(new IntegerSchema()._default(1))
                );
            }
            return operation;
        };
    }
}
