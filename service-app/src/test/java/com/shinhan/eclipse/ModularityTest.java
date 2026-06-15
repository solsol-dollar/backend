package com.shinhan.eclipse;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules.of(ServiceApplication.class).verify();
    }
}
