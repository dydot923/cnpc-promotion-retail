package com.cnpc.promoretail.member;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/identify")
    public ApiResponse<MemberResponse> identify(@Valid @RequestBody MemberIdentifyRequest request) {
        return ApiResponse.ok(memberService.identify(request));
    }

    @GetMapping
    public ApiResponse<List<MemberResponse>> members() {
        return ApiResponse.ok(memberService.members());
    }

    @PostMapping
    public ApiResponse<MemberResponse> create(@Valid @RequestBody MemberCreateRequest request) {
        return ApiResponse.ok(memberService.create(request));
    }

    @GetMapping("/{memberCode}")
    public ApiResponse<MemberResponse> member(@PathVariable String memberCode) {
        return ApiResponse.ok(memberService.getMember(memberCode));
    }

    @PutMapping("/{memberCode}")
    public ApiResponse<MemberResponse> update(
            @PathVariable String memberCode,
            @RequestBody MemberUpdateRequest request
    ) {
        return ApiResponse.ok(memberService.update(memberCode, request));
    }

    @PostMapping("/{memberCode}/points")
    public ApiResponse<PointsChangeResponse> changePoints(
            @PathVariable String memberCode,
            @Valid @RequestBody PointsChangeRequest request
    ) {
        return ApiResponse.ok(memberService.changePoints(memberCode, request));
    }

    @GetMapping("/{memberCode}/points")
    public ApiResponse<List<MemberPointsChangeResponse>> pointsHistory(
            @PathVariable String memberCode,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(memberService.pointsHistory(memberCode, limit));
    }

    @GetMapping("/{memberCode}/coupons")
    public ApiResponse<MemberCouponListResponse> coupons(@PathVariable String memberCode) {
        return ApiResponse.ok(memberService.coupons(memberCode));
    }

    @PostMapping("/{memberCode}/activation-coupons")
    public ApiResponse<MemberCouponListResponse> issueActivationCoupons(@PathVariable String memberCode) {
        return ApiResponse.ok(memberService.issueActivationCoupons(memberCode));
    }
}
