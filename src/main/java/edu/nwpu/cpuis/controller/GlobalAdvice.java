package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalAdvice {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> exception(Exception e) {
        log.error ("global err " + e);
        e.printStackTrace ();
        return Response.serverErr ();
    }

    @InitBinder
    public void init(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr ();
        log.info ("收到请求 ip {}", map.getOrDefault (remoteAddr, remoteAddr));
    }

    private static final Map<String, String> map = new HashMap<String, String> () {
        {
            put ("10.69.231.168", "付佳正");
        }
    };

    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> badRequest(ServletException e) {
        log.error ("global err " + e);
        e.printStackTrace ();
        return Response.fail (HttpStatus.BAD_REQUEST.getReasonPhrase () + ":" + e.getMessage ());
    }

    //验证失败
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> bindException(MethodArgumentNotValidException e) {
        log.error ("请求参数错误 " + e);
        e.printStackTrace ();
        return Response.fail ("请求参数错误 :\n" + HttpStatus.BAD_REQUEST.getReasonPhrase () + ":" + e.getMessage ());
    }

    //自定义错误码错误
    @ExceptionHandler(CpuisException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> cpuisException(CpuisException e) {
        log.error ("自定义错误码错误：CpuisException " + e);
        e.printStackTrace ();
        if (e.getData () != null) {
            return Response.ofFailed (e.getData (), e.getReason ());
        } else
            return Response.ofFailed (e.getReason ());
    }
}
