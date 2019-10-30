package de.kaleidox.util.eval;



public class ExecutionFactory {
    private final StringBuilder code;
    private String originalCode;

    public static class Execution {
        private StringBuilder code;
        private String originalCode;

        public Execution(StringBuilder code, String originalCode) {
            this.code = code;
            this.originalCode = originalCode;
        }

        public String getOriginalCode() {
            return originalCode;
        }

        @Override
        public String toString() {
            return "\t" + this.code.toString().trim();
        }
    }
    public ExecutionFactory() {
        this.code = new StringBuilder();
        this.addPolyfills();
        this.addRunnerWrapper();
    }

    private void addPolyfills() {
        /*
           this.code
                .append(Polyfill.timeout);
         */
    }

    private void addCode(String[] lines) throws ClassNotFoundException {
        this.code.append("function run(){\r\n");
        StringBuilder code = new StringBuilder();

        boolean append;
        for (String line : lines) {
            append = !line.contains("```");
            if (line.startsWith("import ")) {
                append = false;

                String classname = line.substring("import ".length(), line.length() - ((line.lastIndexOf(';') == line.length()) ? 2 : 1));
                Class<?> aClass = Class.forName(classname);

                code.append('\n')
                        .append("\tvar sys = Java.type('java.lang.System')\r\n")
                        .append("\tvar ")
                        .append(aClass.getSimpleName())
                        .append(" = Java.type('")
                        .append(classname)
                        .append("')")
                        .append("\r\n");
            }

            if (append) {
                code.append("\r\n")
                        .append(line.replaceAll("\"", "'"));
            }
        }
        this.originalCode = code.toString();
        this.code
                .append(this.originalCode)
                .append("\r\n}");
    }

    private void safeAddCode(String[] lines){
        this.code.append("function run(){\r\n");
        StringBuilder code = new StringBuilder();

        boolean append;
        for (String line : lines) {
            append = !line.contains("```");
            if (line.startsWith("import ")) {
                append = false;

                String classname = line.substring("import ".length(), line.length() - ((line.lastIndexOf(';') == line.length()) ? 2 : 1));
                String simpleClassName;

                try {
                    Class<?> aClass = Class.forName(classname);
                    simpleClassName = aClass.getSimpleName();
                } catch (ClassNotFoundException e){
                    simpleClassName = "__CLASS_NOT_FOUND__";
                }
                code.append('\n')
                        .append("\tvar sys = Java.type('java.lang.System')\r\n")
                        .append("\tvar ")
                        .append(simpleClassName)
                        .append(" = Java.type('")
                        .append(classname)
                        .append("')")
                        .append("\r\n");
            }

            if (append) {
                code.append("\r\n")
                        .append(line.replaceAll("\"", "'"));
            }

        }
        this.originalCode = code.toString();
        this.code
                .append(this.originalCode)
                .append("\r\n}");
    }

    private void addRunnerWrapper() {
        this.code
                .append("(function(){\r\n")
                .append("var t0 = new Date().getTime();\r\n")
                .append("var result = run.apply(null, arguments);")
                .append("var t1 = new Date().getTime();\r\n")
                .append("return result || 'execution time: ' +Math.floor(t1 - t0) + 'ms';\r\n")
                .append("})(this);");
    }

    public Execution build(String[] lines) throws ClassNotFoundException {
        this.addCode(lines);
        return new Execution(this.code, originalCode);
    }
    public Execution _safeBuild(String[] lines) {
        this.safeAddCode(lines);
        return new Execution(this.code, originalCode);
    }


}
