package org.openelisglobal.hibernate.search.massindexer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class MassIndexerRestController {

    @Autowired
    MassIndexerService massIndexerService;

    @GetMapping("/reindex")
    public ResponseEntity<Boolean> reindex() {
        try {
            massIndexerService.reindex();
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
}
