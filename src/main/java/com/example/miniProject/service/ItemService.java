package com.example.miniProject.service;

import com.example.miniProject.dto.*;
import com.example.miniProject.entity.SalesItemEntity;
import com.example.miniProject.repository.SalesItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {
    private final SalesItemRepository repository;

    // Create Item
    public ResponseDto createItem(ItemDto dto) {
        SalesItemEntity newItemEntity = new SalesItemEntity();
        newItemEntity.setTitle(dto.getTitle());
        newItemEntity.setDescription(dto.getDescription());
        newItemEntity.setMinPriceWanted(dto.getMinPriceWanted());
        newItemEntity.setStatus("판매중");
        newItemEntity.setWriter(dto.getWriter());
        newItemEntity.setPassword(dto.getPassword());
        repository.save(newItemEntity);

        return new ResponseDto("등록이 완료되었습니다.");
    }

    // Read one
    public ResponseItemDto readItem(Long id) {
        Optional<SalesItemEntity> optionalSalesItemEntity =
                repository.findById(id);
        if (optionalSalesItemEntity.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        else return ResponseItemDto.fromEntity(optionalSalesItemEntity.get());
    }

    // Read All
    public Page<ResponseItemPageDto> searchAllItem(Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), limit,
                Sort.by("id").ascending()
        );
        return repository.findAll(pageable)
                .map(ResponseItemPageDto::fromEntity);
    }

    // Update Item
    public ResponseDto updateItem(Long id, ItemDto dto) {
        Optional<SalesItemEntity> optionalSalesItemEntity = repository.findById(id);
        if (optionalSalesItemEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        SalesItemEntity targetItemEntity = optionalSalesItemEntity.get();
        if (!targetItemEntity.getPassword().equals(dto.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (!targetItemEntity.getWriter().equals(dto.getWriter())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        targetItemEntity.setTitle(dto.getTitle());
        targetItemEntity.setDescription(dto.getDescription());
        targetItemEntity.setMinPriceWanted(dto.getMinPriceWanted());
        targetItemEntity.setWriter(dto.getWriter());
        targetItemEntity.setPassword(dto.getPassword());
        repository.save(targetItemEntity);

        return new ResponseDto("물품이 수정되었습니다.");
    }

    // Update Image
    public ResponseDto updateImage(Long id, MultipartFile multipartFile, String writer, String password) {
        // 사용자가 프로필 이미지를 업로드 한다.
        // 1. 유저 존재 확인
        SalesItemEntity targetItemEntity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        validateWriterAndPassword(targetItemEntity, writer, password);

        // media/filename.png
        // media/<업로드 시각>.png
        // 2. 파일을 어디에 업로드 할건지
        // media/{userId}/profile.{파일 확장자}

        // 2-1. 폴더만 만드는 과정
        String profileDir = String.format("media/%d/",id);
        log.info(profileDir);
        try {
            Files.createDirectories(Path.of(profileDir));
        } catch (IOException e){
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // 2-2. 확장자를 포함한 이미지 이름만들기 (profile.{확장자})
        String originalFilename = multipartFile.getOriginalFilename();
        // queue.png => fileNameSplit = {"queue", "png"}
        String[] fileNameSplit = originalFilename.split("\\.");
        // "png" 확장자 가져오기
        String extension = fileNameSplit[fileNameSplit.length - 1];
        String profileFilename = "image." + extension;
        log.info(profileFilename);

        // 2-3. 폴더와 파일 경로를 포함한 이름 만들기
        String profilePath = profileDir + profileFilename;
        log.info(profilePath);

        // 3. MultipartFile 을 저장하기
        try {
            multipartFile.transferTo(Path.of(profilePath));
        } catch (IOException e){
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 4. UserEntity 업데이트 (정적 프로필 이미지를 회수할 수 있는 URL)
        // http://localhost:8080/static/1/image.png
        log.info(String.format("/static/%d/%s", id, profileFilename));
        targetItemEntity.setImageUrl(String.format("/static/%d/%s", id, profileFilename));
        repository.save(targetItemEntity);

        return new ResponseDto("이미지가 등록되었습니다.");
    }

    // Delete Item
    public ResponseDto deleteItem(Long id, RequestUserDto dto) {
        Optional<SalesItemEntity> optionalSalesItemEntity = repository.findById(id);
        if (optionalSalesItemEntity.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        SalesItemEntity targetItemEntity = optionalSalesItemEntity.get();
        if (!targetItemEntity.getPassword().equals(dto.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (!targetItemEntity.getWriter().equals(dto.getWriter())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        repository.deleteById(id);

        return new ResponseDto("물품을 삭제했습니다.");
    }

    // password, writer 일치여부 판별
    private void validateWriterAndPassword(SalesItemEntity itemEntity, String writer, String password) {
        if (!itemEntity.getPassword().equals(password) || !itemEntity.getWriter().equals(writer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
