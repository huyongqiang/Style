package com.yalin.style.domain.interactor;

import com.yalin.style.domain.GalleryWallpaper;
import com.yalin.style.domain.executor.PostExecutionThread;
import com.yalin.style.domain.executor.SerialThreadExecutor;
import com.yalin.style.domain.executor.ThreadExecutor;
import com.yalin.style.domain.repository.SourcesRepository;

import java.util.List;

import io.reactivex.Observable;

import javax.inject.Inject;

/**
 * @author jinyalin
 * @since 2017/5/24.
 */
public class GetGalleryWallpaper extends UseCase<List<GalleryWallpaper>, Void> {
    private SourcesRepository sourcesRepository;

    @Inject
    public GetGalleryWallpaper(ThreadExecutor threadExecutor,
                               SerialThreadExecutor serialThreadExecutor,
                               PostExecutionThread postExecutionThread,
                               SourcesRepository sourcesRepository) {
        super(threadExecutor, serialThreadExecutor, postExecutionThread);
        this.sourcesRepository = sourcesRepository;
    }

    @Override
    Observable<List<GalleryWallpaper>> buildUseCaseObservable(Void aVoid) {
        return sourcesRepository.getWallpaperRepository().getGalleryWallpapers();
    }
}
