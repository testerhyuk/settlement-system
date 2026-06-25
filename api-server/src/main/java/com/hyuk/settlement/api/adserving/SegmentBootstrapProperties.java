package com.hyuk.settlement.api.adserving;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.segment-bootstrap")
public class SegmentBootstrapProperties {
    private boolean enabled = true;
}
