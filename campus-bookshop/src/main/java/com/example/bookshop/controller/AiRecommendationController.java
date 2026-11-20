package com.example.bookshop.controller;

import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.AiRecommendationService;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
@RequestMapping("/shop/ai")
public class AiRecommendationController {
    public static final String STATE="aiReaderState";
    private static final String BASE="redirect:/shop/ai";
    private final AiRecommendationService ai;
    public AiRecommendationController(AiRecommendationService ai) { this.ai=ai; }

    // Intentionally not serializable: credentials must not be persisted with sessions.
    private static final class State {
        final Long owner;
        final String csrf=UUID.randomUUID().toString();
        final AtomicBoolean busy=new AtomicBoolean();
        volatile String key;
        volatile String need="";
        volatile String budget="";
        volatile AiRecommendationService.Result result;
        State(Long owner) { this.owner=owner; }
    }
    private State state(HttpSession session) {
        LoginUser user=(LoginUser)session.getAttribute(AuthInterceptor.SESSION_KEY);
        if(user==null || !"READER".equals(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        synchronized(session) {
            State state=(State)session.getAttribute(STATE);
            if(state==null || !state.owner.equals(user.getId())) {
                state=new State(user.getId()); session.setAttribute(STATE,state);
            }
            return state;
        }
    }
    private State checked(HttpSession session,String csrf) {
        State s=state(session);
        if(!s.csrf.equals(csrf)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"页面已过期，请刷新");
        return s;
    }
    @GetMapping
    public String page(HttpSession session,HttpServletResponse response,Model model) {
        response.setHeader("Cache-Control","no-store");
        State s=state(session);
        model.addAttribute("configured",s.key!=null); model.addAttribute("csrf",s.csrf);
        model.addAttribute("need",s.need); model.addAttribute("budget",s.budget); model.addAttribute("result",s.result);
        return "shop/ai";
    }
    @PostMapping("/clear")
    public String clear(@RequestParam String csrf,HttpSession session,RedirectAttributes ra) {
        State s=checked(session,csrf);
        s.key=null; s.result=null;
        session.removeAttribute(STATE);
        ra.addFlashAttribute("msg","已清除密钥和推荐记录；已经发送的 API 请求无法撤回"); return BASE;
    }
    @PostMapping("/recommend")
    public String recommend(@RequestParam String need,@RequestParam(defaultValue="") String budget,
                            @RequestParam(defaultValue="") String apiKey,
                            @RequestParam String csrf,HttpSession session,RedirectAttributes ra) {
        State s=checked(session,csrf);
        String key;
        synchronized(s) {
            if(!s.busy.compareAndSet(false,true)) { ra.addFlashAttribute("error","推荐正在生成，请勿重复提交"); return BASE; }
            key=s.key;
        }
        try {
            s.need=need.length()>1000?need.substring(0,1000):need; s.budget=budget.length()>30?"":budget; s.result=null;
            if(!apiKey.isBlank()) {
                key=apiKey.trim();
                if(!key.matches("[A-Za-z0-9_-]{16,256}")) throw new BizException("密钥格式不正确，请复制完整的 DeepSeek API 密钥");
                s.key=key;
            }
            if(key==null) throw new BizException("请输入自己的 API 密钥");
            BigDecimal amount=null;
            if(!budget.isBlank()) {
                try { amount=new BigDecimal(budget); } catch(NumberFormatException e) { throw new BizException("请输入有效预算金额"); }
            }
            var result=ai.recommend(key,need,amount);
            // A cleared/logged-out/replaced session must never receive an in-flight result.
            if(session.getAttribute(STATE)==s && s.key!=null) s.result=result;
        } catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        catch(IllegalStateException expired) { return "redirect:/login"; }
        finally { s.busy.set(false); }
        return BASE;
    }
    @PostMapping("/save")
    public String save(@RequestParam String csrf,@RequestParam String name,
                       @RequestParam(required=false) List<Long> selected,HttpSession session,RedirectAttributes ra) {
        State s=checked(session,csrf);
        try {
            var user=(LoginUser)session.getAttribute(AuthInterceptor.SESSION_KEY);
            Long id=ai.save(s.result,selected,name,user);
            ra.addFlashAttribute("msg","已保存为个人学习书单");
            return "redirect:/shop/reading-lists/"+id;
        } catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); return BASE; }
    }
}
