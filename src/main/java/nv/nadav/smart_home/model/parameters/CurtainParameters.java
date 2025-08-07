package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;

import java.util.ArrayList;
import java.util.List;

import static nv.nadav.smart_home.constants.Constants.MAX_POSITION;
import static nv.nadav.smart_home.constants.Constants.MIN_POSITION;
import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;

public class CurtainParameters extends DeviceParameters {
    private Integer position;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public Validators.ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        if (position != null) {
            Validators.ValidationResult result = verifyTypeAndRange(position, "position", Integer.class, List.of(MIN_POSITION, MAX_POSITION));
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }

        if (errors.isEmpty()) {
            return new Validators.ValidationResult(true, null);
        } else {
            return new Validators.ValidationResult(false, errors);
        }
    }
}
