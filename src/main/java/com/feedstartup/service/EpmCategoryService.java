package com.feedstartup.service;

import com.feedstartup.dto.EpmCategoryAdminDto;
import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmCategoryRequestDto;
import com.feedstartup.dto.PageDto;

import java.util.List;

public interface EpmCategoryService {

    /** Every category in display order - backs the public "Filter by Category" sidebar. */
    List<EpmCategoryDto> listPublic();

    /** Every category with how many events use it. */
    List<EpmCategoryAdminDto> listAdmin();

    /** The admin's Categories table: the same rows a page at a time, {@code page} 0-based. */
    PageDto<EpmCategoryAdminDto> pageAdmin(int page, int size);

    EpmCategoryAdminDto create(EpmCategoryRequestDto dto);

    /** Renaming a category also renames it on every event that uses it. */
    EpmCategoryAdminDto update(Long id, EpmCategoryRequestDto dto);

    /** Refused while any event still uses the category. */
    void delete(Long id);

    /** The canonical stored name for {@code raw} (case-insensitive match), or an error listing the options. */
    String resolveName(String raw);
}
