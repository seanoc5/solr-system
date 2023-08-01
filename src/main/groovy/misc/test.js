var list = [1, 'a', 2, 'b', 3, 'c', 4, 'd', 5, 'e'];

for (var i = 0; i < list.length/2; i++) {
    var j = i * 2
    console.info(list[j] + ") My test: " + list[j+1]);
}

// sample postman test for visualization

var template = `
<style>
    .grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        grid-gap: 10px;
    }

    div.grid > div {
        border: 1px solid rgb(233 171 88);
        border-radius: 5px;
        background-color:#eee;
        padding: 1em;
        color: #d9480f;
    }
    .field {
        color: black;
        background-color: lightgray;
        font-weight:bold;
    }

    .facets { color: black; }
    .label { color: green; float: left; width:200px; }
    .value { color: blue; text-wrap: wrap;}
</style>

<div class="grid">
    <div class="facets">
    <div>Facets:...</div>
    
    {{#each fmap as |values fld|}}
        <div class='field'>{{fld}}</div> 
        {{#each values}}
            <div class="label">{{@key}}</div><div class="value">{{this}}</div>
        {{/each}}
    {{/each}}
    </div>

    <div>
        <div>numFound: <b>{{response.numFound}}</b></div>
        <div>qTime: <b>{{header.QTime}}</b></div>
    </div>
    <div>
    <div class="heading">Params: </div>
    {{#each header.params}}
        <div class='label'>{{@key}}</div><div class="value">{{this}}</div>
    {{/each}}    
    </div>
</div>

<div class="label">Parsed Query:</div>
{{#each dismaxLines}}
    <div class="dismax">{{this}}</div>
{{/each}}

</div>
<table bgcolor="#FFFFFF">
    <tr>
        <th>#</th>
        <th>Name</th>
        <th>Type</th>
        <th>Crawl Name</th>
        <th>Score</th>
        <th>Index Time</th>
        <th>id</th>
    </tr>
    {{#each docs}}
    <tr>
        <td>{{@index}}</td>
        <td>{{this.name_txt_en}}</td>
        <td>{{this.type_s}}</td>
        <td>{{this.crawlName_s}}</td>
        <td>{{this.score}}</td>
        <td>{{this.indexedTime_dt}}</td>
        <td>{{this.id}}</td>
    </tr>
    {{/each}}
</table>
`;

var jsonData = pm.response.json();
var resp =  jsonData.response;
var facetFields = jsonData.facet_counts.facet_fields;

var fmap = {};
for(const field in facetFields){
    let vals = facetFields[field];
    console.info(`${field} --- `);
    var valMap = {};
    for (var i = 0; i < vals.length/2; i++) {
        var j = i * 2;
        var term = vals[j];
        var cnt = vals[j+1];
        valMap[term] = cnt;
        console.info(`\t\t${term} --> ${cnt}`);
    }
    fmap[field] = valMap;
}

console.warn("--------------------- test --------------------");
console.info(fmap)


// Set visualizer
pm.visualizer.set(template, {
    // Pass the response body parsed as JSON as `data`
    header: jsonData.responseHeader,
    response: resp,
    facetFields: facetFields,
    fmap: fmap,
    docs: resp.docs
});
