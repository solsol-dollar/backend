package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.ipo.FavoriteIpoItem;
import com.shinhan.eclipse.service.ipo.IpoExplorationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final IpoExplorationService ipoExplorationService;

    /** IPO-005: 관심 IPO 목록 */
    @GetMapping("/ipos")
    public ResponseEntity<ApiResponse<List<FavoriteIpoItem>>> getFavoriteIpos(
            @RequestParam(required = false) Integer limit,
            @UserHeader Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getFavoriteIpos(userId, limit)));
    }
}
