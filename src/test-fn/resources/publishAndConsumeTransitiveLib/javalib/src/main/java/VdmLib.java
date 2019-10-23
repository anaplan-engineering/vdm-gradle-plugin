import org.overture.interpreter.values.Value;
import org.overture.interpreter.values.ValueFactory;

import java.time.Year;

public class VdmLib {

    public static Value addOne(Value nat) throws Exception {
        return ValueFactory.mkNat(nat.natValue(null) + 1);
    }

}