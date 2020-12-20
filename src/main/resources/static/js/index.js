const access = (function () {
    let limit = 20,
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
        $.get(`/api/access?page=${page}&limit=${limit}`)
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
    const progress = {};

    const doUpload = function (container) {
        for (let i = 0; i < container[0].files.length; i++) {
            const form = new FormData();
            form.append("file", container[0].files[i]);
            const elem = $(`<tr>
                                <td class="uuid"></td>
                                <td>${container[0].files[i].name}</td>
                                <td class="time"></td>
                                <td class="stat">正在上传</td>
                            </tr>`);
            $('#upload-display').prepend(elem);
            $.ajax({
                url        : '/api/upload',
                data       : form,
                cache      : false,
                contentType: false,
                processData: false,
                method     : 'POST',
            }).done(function (data) {
                const prog = $('<div class="ui indicating progress"><div class="bar"><div class="progress"></div></div></div>')
                progress[data.uuid] = prog;

                elem.children('.uuid').html(data.uuid);
                elem.children('.time').html(data.time);
                prog.progress({
                    total: data.size,
                    value: 0,
                });

                console.log(data);
                elem.children('.stat').html(prog);
            }).fail(function () {
                elem.addClass('error').children('.stat').html('上传失败');
            });
        }
    };

    setInterval(function () {
        $.get('/api/status')
         .done(function (data) {
             const uuids = new Set();
             $.each(data.data, (_, task) => {
                 if (progress.hasOwnProperty(task.uuid)) {
                     progress[task.uuid].progress('set progress', task.finish);
                     uuids.add(task.uuid);
                 }
             });
             $.each(progress, (uuid, elem) => {
                 if (!uuids.has(Number(uuid))) {
                     elem.progress('complete');
                 }
             });
             const server = $('#server-status');
             if (data['remain'] === 0) {
                 server.progress('set label', '就绪');
                 server.progress('complete');
             } else {
                 server.progress('set total', data['remain'] + data['finish']);
                 server.progress('set progress', data['finish']);
                 server.progress('set label', `剩余${data.data.length}个文件共${data['remain']}条记录`);
             }
         })
    }, 1500);

    return {
        doUpload: doUpload,
    };
})();

$(document).ready(function () {
    $('.tabular.menu .item').tab();
    $('button.left.floated').popup({
        inline    : true,
        hoverable : true,
        lastResort: true,
        popup     : $('.popup'),
        position  : 'top right',
    });
    $('#server-status').progress();
    $('.tabular.menu .item[data-tab="access"]').click();
    access.gotoPage(1);
});