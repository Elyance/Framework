package src.classe;

import java.lang.reflect.Method;

public class Route {
    private Object controllerInstance;
    private Method method;
    private String arg;
    private String methodeHttp;

    public Route(Object controllerInstance, Method method, String methodeHttp) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.methodeHttp = methodeHttp;
    }
    public String getMethodeHttp() {
        return methodeHttp;
    }
    public void setMethodeHttp(String methodeHttp) {
        this.methodeHttp = methodeHttp;
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
