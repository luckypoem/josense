package com.netroby.josense.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
open class AdminController {
    @GetMapping("/admin")
    fun home(): String {
        return "admin"
    }
    @GetMapping("/admin/list")
    fun list(): String {
        return "admin/list"
    }
}