package org.hyeong.booe.contract.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractFormDataTest {

    @Test
    @DisplayName("create: contract와 formJson을 보관한다")
    void create() {
        Contract contract = new Contract();
        String json = "{\"address\":\"서울시\"}";

        ContractFormData formData = ContractFormData.create(contract, json);

        assertThat(formData.getContract()).isEqualTo(contract);
        assertThat(formData.getFormJson()).isEqualTo(json);
    }

    @Test
    @DisplayName("update: formJson만 갱신된다")
    void update() {
        ContractFormData formData = ContractFormData.create(new Contract(), "{}");

        formData.update("{\"updated\":true}");

        assertThat(formData.getFormJson()).isEqualTo("{\"updated\":true}");
    }
}
