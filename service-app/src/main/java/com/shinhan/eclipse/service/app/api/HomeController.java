package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.home.AssetsSummaryResponse;
import com.shinhan.eclipse.service.home.HomeService;
import com.shinhan.eclipse.service.ipo.FavoriteIpoItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<AssetsSummaryResponse>> getAssets(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(homeService.getAssets(authUser.userId())));
    }

    @GetMapping("/favorite-ipos")
    public ResponseEntity<ApiResponse<List<FavoriteIpoItem>>> getFavoriteIpos(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(homeService.getRandomFavoriteIpos(authUser.userId())));
    }
}