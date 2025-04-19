package org.example.hyparview.api;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class TestController {

    private final MembershipService membershipService;

    @Autowired
    public TestController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/view")
    public Collection<Member> view(@RequestParam(name = "region") String region) {
        return "ACTIVE".equalsIgnoreCase(region) ?
            membershipService.getActiveMembers() :
            membershipService.getPassiveMembers();
    }

    @GetMapping("/size-limit")
    public int sizeLimit(@RequestParam(name = "region") String region) {
        return "ACTIVE".equalsIgnoreCase(region) ?
            membershipService.getActiveViewSizeLimit() :
            membershipService.getPassiveViewSizeLimit();
    }
}
