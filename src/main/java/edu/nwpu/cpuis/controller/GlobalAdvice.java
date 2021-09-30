package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.ServletException;

/**
 * @author fujiazheng
 */
@RestControllerAdvice
@Slf4j
public class GlobalAdvice {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<?> exception(Exception e) {
        log.error ("global err " + e);
        e.printStackTrace ();
        return Response.fail ("服务器异常");
    }

    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> badRequest(ServletException e) {
        log.error ("global err " + e);
        e.printStackTrace ();
        return Response.fail (HttpStatus.BAD_REQUEST.getReasonPhrase () + ":" + e.getMessage ());
    }
}
