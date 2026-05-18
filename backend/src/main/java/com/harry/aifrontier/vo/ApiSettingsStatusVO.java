package com.harry.aifrontier.vo;

import lombok.Data;

@Data
public class ApiSettingsStatusVO {

    private ApiCredentialStatusVO bailian;

    private ApiCredentialStatusVO github;

    private ApiCredentialStatusVO mimo;
}
