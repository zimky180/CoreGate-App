package com.zimky.coregateUS.core

object RootProcesses {
    val daemonCheck: String = """
        cg_daemon() {
            case "${'$'}1" in ''|*[!0-9]*) return 1;; esac
            [ "${'$'}1" -gt 1 ] || return 1
            [ -r /proc/${'$'}1/cmdline ] || return 1
            cg_args=${'$'}(tr '\000' '\n' < /proc/${'$'}1/cmdline 2>/dev/null)
            cg_oldifs=${'$'}IFS; IFS='
'
            set -f; set -- ${'$'}cg_args; set +f; IFS=${'$'}cg_oldifs
            case "${'$'}{1##*/}" in sh|ash|bash|dash|mksh|busybox) shift;; *) return 1;; esac
            [ "${'$'}{1:-}" != sh ] || shift
            [ "${'$'}{1:-}" != -- ] || shift
            case "${'$'}{1:-}" in /data/adb/modules/CoreGate/CoreGate.sh|/data/adb/CoreGate/CoreGate.sh) return 0;; *) return 1;; esac
        }
    """.trimIndent()

    data class Entry(val pid: Int, val parent: Int, val started: String, val name: String, val command: String, val lsp: Boolean, val script: Boolean, val scriptPath: String) {
        val coreDaemon: Boolean get() = scriptPath == "/data/adb/modules/CoreGate/CoreGate.sh" || scriptPath == "/data/adb/CoreGate/CoreGate.sh"
        val protected: Boolean get() = coreDaemon || lsp || pid <= 1
    }

    private val enumerate = """
        [ "${'$'}(id -u)" = 0 ] || { echo 'Root 授权不可用' >&2; exit 1; }
        for cg_d in /proc/[0-9]*; do
            cg_pid=${'$'}{cg_d##*/}
            [ "${'$'}cg_pid" != "${'$'}${'$'}" ] || continue
            IFS= read -r cg_name < "${'$'}cg_d/comm" 2>/dev/null || continue
            case "${'$'}cg_name" in sh|ash|bash|dash|mksh|busybox|*.sh|*lspd*|*lsposed*|*LSPosed*|*lspatch*|app_process*) ;; *) continue;; esac
            cg_args=${'$'}(tr '\000' '\n' < "${'$'}cg_d/cmdline" 2>/dev/null)
            cg_oldifs=${'$'}IFS; IFS='
'
            set -f; set -- ${'$'}cg_args; set +f; IFS=${'$'}cg_oldifs
            cg_exe=${'$'}{1##*/}; cg_lsp=0; cg_script=0; cg_script_path=''
            case "${'$'}cg_exe ${'$'}cg_name" in *lspd*|*lsposed*|*LSPosed*|*lspatch*) cg_lsp=1;; esac
            case "${'$'}cg_exe" in app_process*)
                for cg_arg do case "${'$'}cg_arg" in org.lsposed.lspd.*|org.lsposed.lspatch.*) cg_lsp=1;; esac; done;;
            esac
            if [ "${'$'}cg_lsp" = 0 ]; then
                case "${'$'}cg_exe" in sh|ash|bash|dash|mksh|busybox) shift;; *.sh) ;; *) continue;; esac
                case "${'$'}{1:-}" in sh|ash|bash|dash|mksh) shift;; esac
                cg_reject=0
                while [ "${'$'}#" -gt 0 ]; do
                    case "${'$'}1" in --) shift; break;; -*c*|-s|--command*) cg_reject=1; break;; -*) shift;; *) break;; esac
                done
                [ "${'$'}cg_reject" = 0 ] || continue
                cg_script_path=${'$'}{1:-}
                case "${'$'}cg_script_path" in *.sh) ;; *) continue;; esac
                case "${'$'}cg_script_path" in /*) ;; *) cg_cwd=${'$'}(readlink "${'$'}cg_d/cwd" 2>/dev/null); [ -n "${'$'}cg_cwd" ] || continue; cg_script_path="${'$'}cg_cwd/${'$'}cg_script_path";; esac
                cg_real=${'$'}(readlink -f "${'$'}cg_script_path" 2>/dev/null); [ -z "${'$'}cg_real" ] || cg_script_path=${'$'}cg_real
                cg_script=1
            fi
            IFS= read -r cg_stat < "${'$'}cg_d/stat" 2>/dev/null || continue
            cg_tail=${'$'}{cg_stat##*) }; set -- ${'$'}cg_tail
            cg_parent=${'$'}2; shift 19; cg_start=${'$'}1
            cg_cmd=${'$'}(tr '\000\t\n' '   ' < "${'$'}cg_d/cmdline" 2>/dev/null)
            cg_ancestor=${'$'}cg_parent; cg_depth=0
            while [ "${'$'}cg_ancestor" -gt 1 ] 2>/dev/null && [ "${'$'}cg_depth" -lt 8 ]; do
                cg_acmd=${'$'}(tr '\000' ' ' < /proc/${'$'}cg_ancestor/cmdline 2>/dev/null)
                case "${'$'}cg_acmd" in *lspd*|*lsposed*|*LSPosed*|*lspatch*) cg_lsp=1; break;; esac
                IFS= read -r cg_astat < /proc/${'$'}cg_ancestor/stat 2>/dev/null || break
                cg_at=${'$'}{cg_astat##*) }; set -- ${'$'}cg_at; cg_ancestor=${'$'}2
                cg_depth=${'$'}((cg_depth+1))
            done
            if [ "${'$'}cg_lsp" = 0 ]; then
                case "${'$'}cg_script_path" in
                    /data/adb/modules/CoreGate/CoreGate.sh|/data/adb/CoreGate/CoreGate.sh) ;;
                    /data/adb/modules/CoreGate/*|/data/adb/CoreGate/*|*/lib/*|*/common/*|*/service.d/*|*/post-fs-data.d/*|*/service.sh|*/post-fs-data.sh|*/boot-completed.sh) continue;;
                esac
            fi
            printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "${'$'}cg_pid" "${'$'}cg_parent" "${'$'}cg_start" "${'$'}cg_lsp" "${'$'}cg_script" "${'$'}cg_name" "${'$'}cg_script_path" "${'$'}cg_cmd"
        done
    """.trimIndent()
    fun load(sh: RootShell, callback: (List<Entry>, String?) -> Unit) {
        sh.exec(enumerate) { code, out, err ->
            val entries = out.lineSequence().mapNotNull { line ->
                val p = line.split('\t', limit = 8)
                if (p.size != 8) null else p[0].toIntOrNull()?.let { pid ->
                    Entry(pid, p[1].toIntOrNull() ?: 0, p[2], p[5], p[7], p[3] == "1", p[4] == "1", p[6])
                }
            }.sortedWith(compareByDescending<Entry> { it.coreDaemon }.thenBy { it.pid }).toList()
            callback(entries, if (code != 0) err.ifBlank { "读取失败，请确认 APK 的 root 授权" } else null)
        }
    }

    fun kill(sh: RootShell, entry: Entry, callback: (Boolean, String) -> Unit) {
        if (entry.protected) { callback(false, "CoreGate / LSP 或系统关键进程已保护；CoreGate 请到设置页停止"); return }
        val command = """
            $daemonCheck
            p=${entry.pid}
            cg_daemon "${'$'}p" && { echo 'CoreGate 已保护，请到设置页停止'; exit 1; }
            IFS= read -r st < /proc/${'$'}p/stat 2>/dev/null || { echo '进程已退出'; exit 1; }
            tail=${'$'}{st##*) }; set -- ${'$'}tail; shift 19
            [ "${'$'}1" = '${entry.started}' ] || { echo 'PID 已被复用，请刷新列表'; exit 1; }
            # Recheck current ancestry immediately before sending the signal.
            ancestor=${'$'}p; depth=0
            while [ "${'$'}ancestor" -gt 1 ] 2>/dev/null && [ "${'$'}depth" -lt 8 ]; do
                cmd=${'$'}(tr '\000' ' ' < /proc/${'$'}ancestor/cmdline 2>/dev/null)
                case "${'$'}cmd" in *lspd*|*lsposed*|*LSPosed*|*lspatch*) echo 'LSP 框架进程已保护'; exit 1;; esac
                IFS= read -r stat < /proc/${'$'}ancestor/stat 2>/dev/null || break
                rest=${'$'}{stat##*) }; set -- ${'$'}rest; ancestor=${'$'}2
                depth=${'$'}((depth+1))
            done
            kill -9 "${'$'}p" 2>/dev/null || { echo '结束进程失败'; exit 1; }
            echo '已发送结束信号'
        """.trimIndent()
        sh.exec(command) { code, out, err -> callback(code == 0, out.trim().ifBlank { err.trim() }) }
    }
}
