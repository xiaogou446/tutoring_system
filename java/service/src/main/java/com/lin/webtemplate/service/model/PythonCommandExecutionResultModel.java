package com.lin.webtemplate.service.model;

import lombok.Data;

/**
 * 功能：Python 命令执行结果。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class PythonCommandExecutionResultModel {

    private boolean success;

    private boolean timeout;

    private int exitCode;

    private String stdout;

    private String stderr;
}
