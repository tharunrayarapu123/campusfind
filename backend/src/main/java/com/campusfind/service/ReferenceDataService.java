package com.campusfind.service;

import com.campusfind.dto.response.BlockWithRoomsDto;
import com.campusfind.dto.response.CategoryDto;
import com.campusfind.dto.response.RoomDto;
import com.campusfind.entity.Block;
import com.campusfind.entity.ItemCategory;
import com.campusfind.entity.Room;
import com.campusfind.repository.BlockRepository;
import com.campusfind.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final BlockRepository blockRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    @Cacheable("blocks")
    public List<BlockWithRoomsDto> getBlocksWithRooms() {
        List<Block> blocks = blockRepository.findAll();
        return blocks.stream().map(block -> {
            List<Room> rooms = roomRepository.findByBlockId(block.getId());
            List<RoomDto> roomDtos = rooms.stream()
                    .map(r -> RoomDto.builder().id(r.getId()).name(r.getName()).build())
                    .collect(Collectors.toList());

            return BlockWithRoomsDto.builder()
                    .id(block.getId())
                    .name(block.getName())
                    .rooms(roomDtos)
                    .build();
        }).collect(Collectors.toList());
    }

    @Cacheable("categories")
    public List<CategoryDto> getCategories() {
        return Arrays.stream(ItemCategory.values())
                .map(cat -> CategoryDto.builder()
                        .key(cat.name())
                        .displayName(formatDisplayName(cat.name()))
                        .build())
                .collect(Collectors.toList());
    }

    private String formatDisplayName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase())
              .append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
