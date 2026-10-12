package com.pulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMonitorRequest {

    @NotBlank(message = "Monitör adı boş bırakılamaz")
    private String name;

    @NotBlank(message = "URL adresi boş bırakılamaz")
    @Pattern(
            regexp = "^(https?://).+",
            message = "URL http:// veya https:// ile başlamalıdır"
    )
    private String url;

    private Long pingIntervalSeconds = 60L;
}