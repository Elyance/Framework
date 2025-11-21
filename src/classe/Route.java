package src.classe;

import java.lang.reflect.Method;

public class Route {
    private Object controllerInstance;
    private Method method;
    private String arg;

    public Route(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
    }

    public Object getControllerInstance() {
        return controllerInstance;
    }

    public Method getMethod() {
        return method;
    }
    public void setArg(String argument) {
        this.arg = argument;
    }
    public String getArg() {
        return arg;
    }
}
