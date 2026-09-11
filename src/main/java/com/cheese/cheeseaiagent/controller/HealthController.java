package com.cheese.cheeseaiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * <p>
 * 提供最简单的存活探针接口，用于负载均衡、容器健康检查或前端联调时
 * 确认后端服务是否正常运行。
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查接口
     *
     * @return 固定返回 "ok"，表示服务正常运行
     */
    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}
