package com.neu.handler;

import com.neu.exception.AccountLockedException;
import com.neu.exception.AccountNotFoundException;
import com.neu.exception.BaseException;
import com.neu.exception.PasswordErrorException;
import com.neu.result.Result;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandle {

    @ExceptionHandler({AccountNotFoundException.class, PasswordErrorException.class})
    public Result<String> handleLoginException(BaseException exception) {
        return Result.error(401, "账号或密码错误");
    }

    @ExceptionHandler(AccountLockedException.class)
    public Result<String> handleAccountLockedException(AccountLockedException exception) {
        return Result.error(403, "账号已被禁用");
    }

    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException exception) {
        int code = exception.getCode() == null ? 500 : exception.getCode();
        return Result.error(code, exception.getMessage());
    }
}


