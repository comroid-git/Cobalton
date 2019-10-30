package de.kaleidox.util.eval;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;


public class EvalFactory {
    public static class Eval {
        private ExecutionFactory.Execution code;
        private ScriptEngine engine;
        private long execTime;

        Eval(ScriptEngine engine, ExecutionFactory.Execution code) {
            this.engine = engine;
            this.code = code;
        }

        public boolean isVerbose() {
            return code.isVerbose();
        }

        public String run() throws ScriptException {
            String result = this.engine.eval(this.code.toString()).toString();
            this.execTime = Long.parseLong(this.engine.getContext().getAttribute("execTime").toString());
            return result;
        }

        public long getExecTime() {
            return this.execTime;
        }

        public String getUserCode() {
            return this.code.getOriginalCode();
        }

        public String getFullCode() {
            return this.code.toString();
        }
    }

    private final ScriptEngineManager mgr = new ScriptEngineManager();
    private final ScriptEngine engine = mgr.getEngineByName("JavaScript");
    private final Bindings bindings = engine.createBindings();
    private final ExecutionFactory builder = new ExecutionFactory();

    public EvalFactory(HashMap<String, Object> bindings) {
        this.bindings.putAll(bindings);
        engine.setBindings(this.bindings, ScriptContext.GLOBAL_SCOPE);
    }

    public Eval prepare(String[] lines) throws ClassNotFoundException {
        ExecutionFactory.Execution code = this.builder.build(lines);
        return new Eval(this.engine, code);
    }
}
