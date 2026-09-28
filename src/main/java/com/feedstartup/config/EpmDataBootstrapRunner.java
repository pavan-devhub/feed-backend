package com.feedstartup.config;

import com.feedstartup.model.DataMigration;
import com.feedstartup.model.EpmCategory;
import com.feedstartup.model.EpmEvent;
import com.feedstartup.model.EpmReview;
import com.feedstartup.model.EpmVenue;
import com.feedstartup.repository.DataMigrationRepository;
import com.feedstartup.repository.EpmCategoryRepository;
import com.feedstartup.repository.EpmEventRepository;
import com.feedstartup.repository.EpmReviewRepository;
import com.feedstartup.repository.EpmVenueRepository;
import com.feedstartup.service.EpmGalleryImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-time data steps for the admin-managed EPM tables, each recorded in {@code data_migrations}
 * so it runs exactly once per database - deleting everything a step created (say, every review)
 * never brings the defaults back on the next restart.
 * <ol>
 *   <li>Categories: the five that used to be a hard-coded enum, plus any other name already on an event.</li>
 *   <li>Venues: one per distinct state/district/place/venue already used by an event.</li>
 *   <li>Reviews: the five testimonials that used to be hard-coded on the EPM page.</li>
 *   <li>Gallery: every image already in the storage folder gets its database row, and the old JSON
 *       metadata sidecars are folded into those rows and deleted (see EpmGalleryImportServiceImpl).</li>
 * </ol>
 * A step that fails is logged and retried on the next startup instead of stopping the app.
 */
@Component
public class EpmDataBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EpmDataBootstrapRunner.class);

    private final DataMigrationRepository migrationRepository;
    private final EpmCategoryRepository categoryRepository;
    private final EpmVenueRepository venueRepository;
    private final EpmReviewRepository reviewRepository;
    private final EpmEventRepository eventRepository;
    private final EpmGalleryImportService galleryImportService;

    public EpmDataBootstrapRunner(DataMigrationRepository migrationRepository,
                                  EpmCategoryRepository categoryRepository,
                                  EpmVenueRepository venueRepository,
                                  EpmReviewRepository reviewRepository,
                                  EpmEventRepository eventRepository,
                                  EpmGalleryImportService galleryImportService) {
        this.migrationRepository = migrationRepository;
        this.categoryRepository = categoryRepository;
        this.venueRepository = venueRepository;
        this.reviewRepository = reviewRepository;
        this.eventRepository = eventRepository;
        this.galleryImportService = galleryImportService;
    }

    @Override
    public void run(String... args) {
        once("epm-categories-seed-v1", this::seedCategories);
        once("epm-venues-seed-v1", this::seedVenues);
        once("epm-reviews-seed-v1", this::seedReviews);
        once("epm-gallery-import-v1", galleryImportService::importFromStorage);
    }

    private void once(String id, Runnable step) {
        if (migrationRepository.existsById(id)) return;
        try {
            step.run();
            migrationRepository.save(new DataMigration(id));
        } catch (RuntimeException e) {
            log.error("EPM data step {} failed - it will be retried on the next startup", id, e);
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        List<EpmCategory> defaults = new ArrayList<>(List.of(
                new EpmCategory("GAP Workshop", "Good Agricultural Practices (GAP Workshop)", "green", 0),
                new EpmCategory("Capacity Building Trainings", "Capacity Building Trainings", "teal", 1),
                new EpmCategory("FPO Management Sessions", "FPO Management Sessions", "purple", 2),
                new EpmCategory("EPM Meeting", "EPM Meeting", "blue", 3),
                new EpmCategory("Export Workshops", "Export Workshops", "orange", 4)));
        // Keep any other category an existing event already uses, so no event is left pointing
        // at a name the admin can't see or pick.
        for (String used : eventRepository.findDistinctCategories()) {
            boolean known = defaults.stream().anyMatch(c -> c.getName().equalsIgnoreCase(used));
            if (!known && !used.isBlank()) {
                defaults.add(new EpmCategory(used.trim(), used.trim(), "gray", defaults.size()));
            }
        }
        categoryRepository.saveAll(defaults);
        log.info("Seeded {} EPM categories.", defaults.size());
    }

    private void seedVenues() {
        if (venueRepository.count() > 0) return;
        Map<String, EpmVenue> distinct = new LinkedHashMap<>();
        for (EpmEvent e : eventRepository.findAll()) {
            if (e.getVenue() == null || e.getCity() == null || e.getDistrict() == null || e.getState() == null) continue;
            String key = String.join("|", e.getState(), e.getDistrict(), e.getCity(), e.getVenue()).toLowerCase(Locale.ROOT);
            distinct.computeIfAbsent(key, k -> {
                EpmVenue v = new EpmVenue();
                v.setName(e.getVenue().trim());
                v.setCity(e.getCity().trim());
                v.setDistrict(e.getDistrict().trim());
                v.setState(e.getState().trim());
                return v;
            });
        }
        venueRepository.saveAll(distinct.values());
        log.info("Seeded {} EPM venues from existing events.", distinct.size());
    }

    private void seedReviews() {
        if (reviewRepository.count() > 0) return;
        String[][] defaults = {
                {"Ramesh", "Farmer", "Great initiative! EPM helped me understand export opportunities clearly."},
                {"Sita", "Entrepreneur", "Informative session with practical insights on global markets."},
                {"Anand", "FPO Member", "Well organized and very impactful meeting for our FPO members."},
                {"Kiran", "Trader", "The networking opportunities were fantastic. Highly recommend attending."},
                {"Lakshmi", "Agri-Business", "Excellent guidance on export documentation and compliance."},
        };
        List<EpmReview> reviews = new ArrayList<>();
        for (int i = 0; i < defaults.length; i++) {
            EpmReview r = new EpmReview();
            r.setAuthorName(defaults[i][0]);
            r.setAuthorRole(defaults[i][1]);
            r.setContent(defaults[i][2]);
            r.setRating(5);
            r.setPublished(true);
            r.setDisplayOrder(i);
            reviews.add(r);
        }
        reviewRepository.saveAll(reviews);
        log.info("Seeded {} EPM reviews.", reviews.size());
    }
}
