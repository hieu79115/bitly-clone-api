package com.thinh.shortener.controller;

import com.thinh.shortener.domain.dto.request.TagRequestDto;
import com.thinh.shortener.domain.dto.response.TagResponseDto;
import com.thinh.shortener.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagResponseDto> createTag(
            @Valid @RequestBody TagRequestDto request,
            Principal principal
    ){
        TagResponseDto response = tagService.createTag(request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TagResponseDto>> getUserTags(Principal principal){
        List<TagResponseDto> response = tagService.getUserTags(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponseDto> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagRequestDto request,
            Principal principal
    ) {
        TagResponseDto response = tagService.updateTag(id, request, principal.getName());
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long id,
            Principal principal
    ) {
        tagService.deleteTag(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

}
