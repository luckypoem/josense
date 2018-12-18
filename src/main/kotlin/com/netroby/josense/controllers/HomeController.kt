package com.netroby.josense.controllers


import com.netroby.josense.config.JosenseConfig
import com.netroby.josense.repository.ArticleRepository
import com.netroby.josense.service.AuthAdapterService
import com.netroby.josense.service.PrepareModelService
import com.netroby.josense.vo.Article
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class HomeController(
        @Autowired private val articleRepository: ArticleRepository,
        @Autowired private val prepareModelService: PrepareModelService,
        @Autowired private val authAdapterService: AuthAdapterService,
        @Autowired private val josenseConfig: JosenseConfig
) {
    private val logger = LoggerFactory.getLogger("home")
    @GetMapping("/")
    fun home(model: Model, @RequestParam(value = "page", defaultValue = "0") page: Int): ModelAndView {
        model.addAllAttributes(prepareModelService.getModel())
        val sort = Sort(Sort.Direction.DESC, "aid")
        val pageable = PageRequest.of(page, 15, sort)
        val result = articleRepository.findAll(pageable)
        model.addAttribute("result", result.content)
        val role = SimpleGrantedAuthority("ROLE_ADMIN")
        model.addAttribute("username", authAdapterService.getUserName())
        model.addAttribute("isAuthenticated", authAdapterService.isAuthenticated())
        model.addAttribute("nextPage", page+1)
        var prevPage = page - 1;
        if (prevPage < 0) {
            prevPage = 0;
        }
        model.addAttribute("prevPage", prevPage)
        return ModelAndView("home")
    }


    @GetMapping("/rss", produces = ["application/xml"])
    @ResponseBody
    fun rss(model: Model, @RequestParam(value = "page", defaultValue = "0") page: Int): String {
        model.addAllAttributes(prepareModelService.getModel())
        val sort = Sort(Sort.Direction.DESC, "aid")
        val pageable = PageRequest.of(page, 15, sort)
        val result = articleRepository.findAll(pageable)
        model.addAttribute("result", result.content)
        val feed = SyndFeedImpl()
        feed.feedType = "atom_1.0"
        feed.title = josenseConfig.site["name"] ?: ""
        feed.description = josenseConfig.site["description"] ?: ""

        val entries = ArrayList<SyndEntry>();
        for (blog: Article in result.content) {

            val entry = SyndEntryImpl()
            entry.title = blog.title

            val content = SyndContentImpl()
            content.value = Jsoup.parse(blog.content).text();

            entry.description = content
            entry.publishedDate = Date(blog.publishTime * 1000L)
            //TODO: replace with real web site domain
            entry.uri = "https://www.netroby.com/view/" + blog.aid
            entries.add(entry)
        }
        // 收集所有的文章，设置为Feed
        feed.entries = entries

        return SyndFeedOutput().outputString(feed)
    }

    @GetMapping("/about")
    fun about(model: Model): ModelAndView {
        model.addAllAttributes(prepareModelService.getModel())
        model.addAttribute("username", authAdapterService.getUserName())
        model.addAttribute("isAuthenticated", authAdapterService.isAuthenticated())
        return ModelAndView("about")
    }

    @GetMapping("/search")
    fun home(model: Model, @RequestParam(value = "page", defaultValue = "0") page: Int, @RequestParam(value = "keyword", defaultValue = "") keyword: String): ModelAndView {
        model.addAllAttributes(prepareModelService.getModel())
        val sort = Sort(Sort.Direction.DESC, "aid")
        val pageable = PageRequest.of(page, 15, sort)
        logger.info("Search by keyword {}", keyword)
        val result = articleRepository.findByContentContainingOrTitleContaining(pageable, "%$keyword%", "%$keyword%")
        model.addAttribute("result", result.content)
        val role = SimpleGrantedAuthority("ROLE_ADMIN")
        model.addAttribute("username", authAdapterService.getUserName())
        model.addAttribute("isAuthenticated", authAdapterService.isAuthenticated())
        model.addAttribute("nextPage", page+1)
        var prevPage = page - 1
        if (prevPage < 0) {
            prevPage = 0
        }
        model.addAttribute("prevPage", prevPage)
        return ModelAndView("search")
    }

    @GetMapping("/view/{id}")
    fun view(model: Model, @PathVariable("id") id: Int): ModelAndView {
        model.addAllAttributes(prepareModelService.getModel())
        val result = articleRepository.findById(id.toLong()) ?: null ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        logger.info(result.toString().substring(0..220))
        val sArticle = result.get()
        sArticle.views = sArticle.views + 1
        articleRepository.save(sArticle)
        model.addAttribute("result", result.get())
        model.addAttribute("username", authAdapterService.getUserName())
        model.addAttribute("isAuthenticated", authAdapterService.isAuthenticated())
        return ModelAndView("view")
    }

    @RequestMapping("/login")
    fun login(): String {
        return "login"
    }

    @RequestMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): String {
        var auth = authAdapterService.getAuthentication()
        if (auth != null) {
            SecurityContextLogoutHandler().logout(request, response, auth)
        }
        return "redirect:/"
    }
}