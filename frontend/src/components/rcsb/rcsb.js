
let queryProteinDNA = {
    "query": {
        "type": "group",
        "logical_operator": "and",
        "nodes": [{
            "type": "terminal",
            "service": "text",
            "parameters": {
                "attribute": "exptl.method",
                "operator": "exact_match",
                "value": "X-RAY DIFFRACTION"
            }
        },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_DNA",
                    "operator": "greater",
                    "value": 1
                }
            },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_protein",
                    "operator": "greater",
                    "value": 0
                }
            }]
    },
    "request_options": {
        "return_all_hits": true,
        "sort": [{
            "sort_by": "rcsb_entry_info.resolution_combined",
            "direction": "asc"
        }]
    },
    "return_type": "entry"
};

let queryDNAonly = {
    "query": {
        "type": "group",
        "logical_operator": "and",
        "nodes": [{
            "type": "terminal",
            "service": "text",
            "parameters": {
                "attribute": "exptl.method",
                "operator": "exact_match",
                "value": "X-RAY DIFFRACTION"
            }
        },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_DNA",
                    "operator": "greater",
                    "value": 1
                }
            },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_protein",
                    "operator": "exact_match",
                    "value": 0
                }
            }]
    },
    "request_options": {
        "return_all_hits": true,
        "sort": [{
            "sort_by": "rcsb_entry_info.resolution_combined",
            "direction": "asc"
        }]
    },
    "return_type": "entry"
};

let queryProteinRNA = {
    "query": {
        "type": "group",
        "logical_operator": "and",
        "nodes": [{
            "type": "terminal",
            "service": "text",
            "parameters": {
                "attribute": "exptl.method",
                "operator": "exact_match",
                "value": "X-RAY DIFFRACTION"
            }
        },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_RNA",
                    "operator": "greater",
                    "value": 1
                }
            },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_protein",
                    "operator": "greater",
                    "value": 0
                }
            }]
    },
    "request_options": {
        "return_all_hits": true,
        "sort": [{
            "sort_by": "rcsb_entry_info.resolution_combined",
            "direction": "asc"
        }]
    },
    "return_type": "entry"
};


let queryRNAonly = {
    "query": {
        "type": "group",
        "logical_operator": "and",
        "nodes": [{
            "type": "terminal",
            "service": "text",
            "parameters": {
                "attribute": "exptl.method",
                "operator": "exact_match",
                "value": "X-RAY DIFFRACTION"
            }
        },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_RNA",
                    "operator": "greater",
                    "value": 1
                }
            },
            {
                "type": "terminal",
                "service": "text",
                "parameters": {
                    "attribute": "rcsb_entry_info.polymer_entity_count_protein",
                    "operator": "exact_match",
                    "value": 0
                }
            }]
    },
    "request_options": {
        "return_all_hits": true,
        "sort": [{
            "sort_by": "rcsb_entry_info.resolution_combined",
            "direction": "asc"
        }]
    },
    "return_type": "entry"
};


function test() {
    //console.log($("#dateBefore").val());
    //console.log($("#dateAfter").val());
}


export const getPDB = async (pdbname) => {
    let result = "";
    let response = await fetch("https://files.rcsb.org/download/" + pdbname + ".pdb")
    const data = await response.text();
    return data;
}


export const submitPDBList = (query = queryProteinDNA) => {
    let request = query;
    //let opt = $("#structureType").val();
    //switch (parseInt(opt)) {
    //    case 1: request = queryProteinDNA; break;
    //    case 2: request = queryDNAonly; break;
    //    case 3: request = queryProteinRNA; break;
    //    case 4: request = queryRNAonly; break;
    //    default: request = queryProteinDNA; break;
    //}
    //let request = "<orgPdbQuery><queryType>org.pdb.query.simple.ChainTypeQuery</queryType><description>Luke Czapla for Wilma Olson lab</description><containsProtein>Y</containsProtein><containsDna>Y</containsDna><containsRna>N</containsRna></orgPdbQuery>";
    fetch('https://search.rcsb.org/rcsbsearch/v1/query', {method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        data: JSON.stringify(request)}).then(data => data.text())
        .then((result) => {
            console.log(result);
            let pdblist = "";
            for (let i = 0; i < result.result_set.length; i++) {
                pdblist += result.result_set[i].identifier + "\n";
            }
            //let pdbdata = {pdbs: "", pdbList: pdblist, RNA: false, cullEigen: $("#cullEigen").is(":checked"),
            //    cullStandard: $("#cullStandard").is(":checked")};
            //console.log(pdbdata);

            //if (parseInt(opt) == 3 || parseInt(opt) == 4) pdbdata.RNA = true;
            //if ($("#specificPdbList").is(':checked')) pdbdata.pdbList = $("#pdbIdList").val();
            //pdbdata.beforeDate = $("#dateBefore").val();
            //pdbdata.afterDate = $("#dateAfter").val();
            //pdbdata.description = $("#ffdescription").val();
            //console.log(pdbdata.beforeDate + " " + pdbdata.afterDate);
            //pdbdata.nonredundant = $("#removeRedundant").is(":checked");
            /*let req = {headers: {
                    'Content-Type': 'application/json'
                },
                url: "/analyzePDBs",
                method: "POST",
                data: JSON.stringify(pdbdata)
            };
            $.ajax(req).done(function(data) {
                if (data.length > 1) alert("Your model is being made and code is " + data);
                else alert("A problem occurred, check parameters and make sure you are logged in");
                refreshData();
            });*/

            //break;

        });

}




