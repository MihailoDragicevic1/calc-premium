package rs.raf.vm;

import lombok.Getter;
import rs.raf.gen.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Value {
    private final Map<Integer, Object> localVariables = new HashMap<>();
    @Getter
    private final List<Object> localStack;
    @Getter
    private final List<Instruction> compiledBody;

    public Value(List<Object> args, List<Instruction> compiledBody) {
        this.localStack = new ArrayList<>(args);
        this.compiledBody = compiledBody;
    }

    public void setLocal(int index, Object value) {
        localVariables.put(index, value);
    }

    public Object getLocal(int index) {
        if (!localVariables.containsKey(index)) {
            throw new IllegalArgumentException("Lokalna promenljiva na indeksu " + index + " ne postoji.");
        }
        return localVariables.get(index);
    }
}
