package com.iitp.financecopilot.services;

import com.iitp.financecopilot.common.ApiException;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FileStorageService {

    private final GridFsTemplate gridFsTemplate;

    public FileStorageService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    public String store(String fileName, String contentType, byte[] bytes) {
        ObjectId id = gridFsTemplate.store(new ByteArrayInputStream(bytes), fileName, contentType);
        return id.toHexString();
    }

    public LoadedFile load(String storageId) {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(storageId))));
        if (file == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bill not found");
        }
        GridFsResource resource = gridFsTemplate.getResource(file);
        try (InputStream in = resource.getInputStream()) {
            return new LoadedFile(
                    file.getFilename(),
                    file.getMetadata() != null && file.getMetadata().getString("_contentType") != null
                            ? file.getMetadata().getString("_contentType")
                            : resource.getContentType(),
                    in.readAllBytes()
            );
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read stored file");
        }
    }

    public void delete(String storageId) {
        if (storageId == null || storageId.isBlank()) {
            return;
        }
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(new ObjectId(storageId))));
    }

    public record LoadedFile(String fileName, String contentType, byte[] bytes) {
    }
}
