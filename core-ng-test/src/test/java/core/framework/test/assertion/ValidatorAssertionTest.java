package core.framework.test.assertion;

import core.framework.api.validate.Length;
import core.framework.api.validate.NotEmpty;
import core.framework.api.validate.NotNull;
import org.junit.jupiter.api.Test;

import static core.framework.test.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
class ValidatorAssertionTest {
    @Test
    void failWithMessage() {
        Bean bean = new Bean();
        bean.field2 = "";

        assertThatThrownBy(() -> assertThat(bean).isValid())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("to be valid, but found some violations:")
                .hasMessageContaining("{field1=field must not be null, field2=field must not be empty}");
    }

    @Test
    void isValid() {
        Bean bean = new Bean();
        bean.field1 = "value";
        assertThat(bean).isValid();
    }

    static class Bean {
        @NotNull
        @Length(max = 5, message = "field1 must not be longer than 5")
        public String field1;

        @NotEmpty
        public String field2;
    }
}
