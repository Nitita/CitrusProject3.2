package autotests;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {

  @Value("${store.url:}")
  private String storeUrl;

  @Value("/v2")
  private String basePath;
}
