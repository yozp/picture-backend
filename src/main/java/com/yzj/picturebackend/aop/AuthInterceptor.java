package com.yzj.picturebackend.aop;

import com.yzj.picturebackend.annotation.AuthCheck;
import com.yzj.picturebackend.exception.BusinessException;
import com.yzj.picturebackend.exception.ErrorCode;
import com.yzj.picturebackend.model.entity.User;
import com.yzj.picturebackend.model.enums.UserRoleEnum;
import com.yzj.picturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验切面类
 * 拦截所有被 @AuthCheck 注解的方法
 */
@Aspect//标识这是一个切面类，定义横切逻辑（如权限校验）
@Component//由 Spring 管理其生命周期，成为 Bean，保证这个切面类能够放入IOC容器
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    // 使用@Around注解标明环绕通知方法
    // （默认）value属性：切入点表达式，告诉Spring当前通知方法要套用到哪个目标方法上
    //整体意思：拦截所有被 @AuthCheck 注解的方法，authCheck 参数用于获取注解属性值
    @Around("@annotation(authCheck)")
    // 在前置通知方法形参位置声明一个JoinPoint类型的参数，Spring就会将这个对象传入
    // 根据JoinPoint对象就可以获取目标方法名称、实际参数列表
    //AuthCheck：自动注入被拦截方法上的 @AuthCheck 注解实例
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 以下为：必须有该权限才通过
        // 获取当前用户具有的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
