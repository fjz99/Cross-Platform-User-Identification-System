package edu.nwpu.cpuis.controller;

import edu.nwpu.cpuis.entity.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<?> badRequest(MissingServletRequestParameterException e) {
        log.error ("global err " + e);
        e.printStackTrace ();
        return Response.fail (HttpStatus.BAD_REQUEST.getReasonPhrase () + ":" + e.getMessage ());
    }
}
