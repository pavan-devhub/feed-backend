package com.feedstartup.dto;

/** One photo inside a district. {@code imageUrl} is the server-relative path it can be streamed
 * from - the disk path itself is never exposed. {@code width}/{@code height} are the pixel size
 * (null if it could not be read), so the page can lay photos out at their true proportions before
 * any of them has loaded. */
public record EpmGalleryPhotoDto(Long id, String imageUrl, Integer width, Integer height, String caption) {}
