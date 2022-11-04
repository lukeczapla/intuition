package org.magicat.controller;

import io.swagger.annotations.ApiOperation;
import org.magicat.MIND.GeneMIND;
import org.magicat.model.SequenceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SequenceController {

    private final GeneMIND geneMIND;

    @Autowired
    public SequenceController(GeneMIND geneMIND) {
        this.geneMIND = geneMIND;
    }

    @ApiOperation(value = "Search for a sequence in the T2T-CHM13 reference genome, chromosomsal/mtDNA")
    @RequestMapping(value = "/sequence/search", method = RequestMethod.POST)
    public List<SequenceItem> search(@RequestBody List<String> seqs) {
        List<SequenceItem> result = new ArrayList<>();
        for (String seq: seqs) {
            List<SequenceItem> items = geneMIND.findSequence(seq, false);
            //if (items.size() == 2) items = List.of(items.get(0));  // first element on
            if (items != null && items.size() > 0) result.addAll(items);
        }
        return result;
    }
}
