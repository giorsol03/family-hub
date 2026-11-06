package com.solomonidis.familyhub.common.web;

import com.solomonidis.familyhub.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * In production the built React app lives in {@code static/}. Deep links such as {@code /members/3}
 * have no file, so forward them to {@code index.html} and let React Router handle them.
 * Paths containing a dot (assets) and {@code /api/**} are never forwarded.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/{p1:[^.]*}", "/{p1:[^.]*}/{p2:[^.]*}", "/{p1:[^.]*}/{p2:[^.]*}/{p3:[^.]*}"})
    public String forward(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api")) {
            throw new NotFoundException("No such API endpoint");
        }
        return "forward:/index.html";
    }
}
