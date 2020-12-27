const access = (function () {
    let limit = 20,
        cpage = undefined,
        close = new Set(),
        table = undefined;

    const htmlPage = function (page, index) {
        return `<a class="${index === page ? 'active' : (index === '...' ? 'disabled' : '')} item"
                   onclick="access.gotoPage(${index})">
                    ${index}
                </a>`;
    };
    const htmlData = function (data) {
        return typeof data === 'boolean' ? `<i class="${data ? 'check' : 'close'} icon"></i>` : (data !== undefined ? data : '');
    }

    const rerender = function () {
        const columns  = Object.keys(Object.assign({}, ...table)),
              filtered = columns.filter(col => !close.has(col));

        $('table.special thead tr').html(filtered.map(col => `<th class="single line">${col}</th>`).join(''));

        let dataHtml = '';
        $.each(table, function (idx, val) {
            dataHtml = dataHtml
                       + '<tr>'
                       + filtered.map(key => `<td><div><div>${htmlData(val[key])}</div></div></td>`).join('')
                       + '</tr>';
        });
        $('table.special tbody').html(dataHtml);

        return columns;
    };

    const viewData = function (page, data) {
        table = data.data;
        const columns = rerender();

        const formHtml = columns.map(col => `<div class="inline field">
                                                 <div class="ui toggle checkbox">
                                                     <input type="checkbox"
                                                            class="hidden"
                                                            onchange="access.doColumn('${col}', $(this)[0].checked)"
                                                            ${close.has(col) ? '' : 'checked'}>
                                                     <label>${col}</label>
                                                 </div>
                                             </div>`).join('');
        $('.popup .form').html(formHtml);
        $('.ui.checkbox').checkbox();

        let pageHtml = '',
            totalCnt = Math.ceil(data.rows / limit),
            currPage = 1;
        for (; currPage < Math.min(4, totalCnt); currPage++) {
            pageHtml += htmlPage(page, currPage);
        }
        if (currPage < page - 2) {
            pageHtml += htmlPage(page, '...');
            currPage = page - 2;
        }
        for (; currPage < Math.min(page + 3, totalCnt); currPage++) {
            pageHtml += htmlPage(page, currPage);
        }
        if (currPage < totalCnt - 2) {
            pageHtml += htmlPage(page, '...');
            currPage = totalCnt - 2;
        }
        for (; currPage <= totalCnt; currPage++) {
            pageHtml += htmlPage(page, currPage);
        }
        $('.pagination').html(pageHtml);
    };

    const gotoPage = function (page) {
        page ? cpage = page : page = cpage;
        $.get(`/api/access?page=${page}&limit=${limit}&search=${$('#search input').val()}`)
         .done(data => viewData(page, data));
    };

    const doColumn = function (col, show) {
        show ? close.delete(col) : close.add(col);
        rerender();
    };

    return {
        gotoPage: gotoPage,
        doColumn: doColumn,
    }
})();

const upload = (function () {
    const doUpload = function (container) {
        for (let i = 0; i < container[0].files.length; i++) {
            const form = new FormData();
            form.append("file", container[0].files[i]);
            const prog = $('<div class="ui indicating progress">' +
                           '    <div class="bar"><div class="progress"></div></div>' +
                           '    <div class="label"></div>' +
                           '</div>');
            prog.progress({
                total: 100,
                value: 0,
            });
            const elem = $(`<tr>
                                <td class="uuid"></td>
                                <td>${container[0].files[i].name}</td>
                                <td class="time"></td>
                                <td class="stat"></td>
                            </tr>`);
            elem.children('.stat').append(prog);
            $('#upload-display').prepend(elem);
            let timer;
            // noinspection JSUnusedGlobalSymbols
            $.ajax({
                xhr        : function () {
                    const xhr = new window.XMLHttpRequest();
                    xhr.upload.addEventListener('progress', function (event) {
                        if (event.lengthComputable) {
                            prog.progress('set total', event.total * 2);
                            prog.progress('set label', '正在上传');
                            prog.progress('set progress', event.loaded);
                            if (event.loaded === event.total) {
                                timer = setInterval(function () {
                                    prog.progress('set progress', prog.progress('get value') + 1000000);
                                }, 1000);
                            }
                        }
                        console.log(event);
                    }, false);
                    return xhr;
                },
                url        : '/api/upload',
                data       : form,
                cache      : false,
                contentType: false,
                processData: false,
                method     : 'POST',
            }).done(function (data) {
                clearInterval(timer);
                prog.progress('set total', data.size);
                prog.progress('set progress', 0);
                prog.progress('set label', '正在处理');
                prog.progress('remove success');
                elem.children('.uuid').html(data.uuid);
                elem.children('.time').html(data.time);
                timer = setInterval(function () {
                    $.get(`/api/status?uuid=${data.uuid}`)
                     .done(function (data) {
                         prog.progress('set progress', data.finish);
                         if (data['remain'] === 0) {
                             prog.progress('set success');
                             prog.progress('set label', '完成');
                             clearInterval(timer);
                         }
                     });
                }, 1000);
            }).fail(function () {
                prog.progress('set error');
                prog.progress('set label', '上传失败');
                clearInterval(timer);
            });
        }
    };

    $(document).ready(function () {
        const prog = $('#server-upload');
        setInterval(function () {
            $.get('/api/status')
             .done(function (data) {
                 if (data['remain'] === 0) {
                     prog.progress('set success');
                     prog.progress('set label', '就绪');
                 } else {
                     prog.progress('set total', data.finish + data['remain']);
                     prog.progress('set progress', data.finish);
                     prog.progress('set label', `正在处理 ${data.finish} / ${data.finish + data['remain']}`)
                 }
             })
        }, 2000);
    });

    return {
        doUpload: doUpload,
    };
})();

$(document).ready(function () {
    // noinspection JSUnresolvedFunction
    $('.tabular.menu .item').tab();
    $('#btn-filter').popup({
        inline    : true,
        hoverable : true,
        lastResort: true,
        popup     : $('#filter'),
        position  : 'top right',
    });
    $('#btn-search').popup({
        inline    : true,
        hoverable : true,
        lastResort: true,
        popup     : $('#search'),
        position  : 'top right',
    });
    $('#search input').blur(() => access.gotoPage());
    $('#server-status').progress();
    $('.tabular.menu .item[data-tab="access"]').click();
    access.gotoPage(1);
});