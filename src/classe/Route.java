package src.classe;

import java.lang.reflect.Method;

public class Route {
    private Object controllerInstance;
    private Method method;

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
}
