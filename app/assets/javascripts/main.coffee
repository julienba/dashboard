# Just a log helper
log = (args...) ->
    console.log.apply console, args if console.log?

document.username = decodeURIComponent( document.cookie.split('mail%3A')[1] )
# ---------------------------------------- ROUTER

class AppRouter extends Backbone.Router
    initialize: ->
        log 'init router'

    routes:
        ''           : 'index'
        'modal'      : 'dummy'
        'tab-:id'    : 'tabs'

    index: ->
        log 'route index'
        # move to first Tab
        if tabs.models.length > 0
            tabId = tabs.models[0].id
            tabs.views[tabId].display2(tabId)

    tabs: (id) ->
        log "route tab: #{id}"
        if tabs.views[id]
            tabs.views[id].display2(id)

    dummy: ->
        log 'do nothing'
    
# ---------------------------------------- MODEL
class Tab extends Backbone.Model

class Module extends Backbone.Model

    url: ->
        '/modules/saveJS'

# ---------------------------------------- COLLECTION

class TabList extends Backbone.Collection
    initialize: (models, options) ->
        @.on('add', @addOne, @)
        @.on('reset', @addAll, @)
        @.on('all', @render, @)

    model: Tab

    views: []

    url : ->
        '/json/tab/' + document.username

    parse: (resp, xhr) ->
        log 'parse'
        resp

    addOne: (tab) ->
        view = new TabView({model: tab})
        $("#tabs-list").append( view.render().el )
        @views[tab.id] = view

    addAll: ->
        log 'addAll'
        $('#tabs-list').html('')
        tabs.each(@.addOne, @)

    render: ->
        log 'render TabList'

class ModuleList extends Backbone.Collection
    model: Module
    
    initialize: (models, options) ->
        @id = options['tabId']
        @.on('reset', @.render, @)
        
    url: ->
        '/json/modules/' + @id

    render: ->
        $('#modules').html('')
        @.each (model) ->
            log 'render modules collection'
            view = new ModuleView({model: model})
            $("#modules").append( view.render().el )

# ---------------------------------------- VIEW
class ModuleView extends Backbone.View
    initialize: ->
        @.model.on( 'change', @.render, @)

    template: MUSTACHE_TEMPLATES['module/show']

    events:
        'click .module .action .icon-refresh' : 'updateModule'
        'click .module .action .icon-pencil'  : 'configModule'
        'click .module .action .icon-remove'  : 'delete'
        'click .feeds a'                      : 'markAsRead'
        'mousedown .feeds a'                  : 'markAsRead' # Tricks for middle click...

    render: ->
        @$el.html Mustache.render(@template, @.model.toJSON())
        this

    updateModule: ->
        jsRoutes.controllers.Modules.fetch(@.model.id).ajax
            context: @
            success: (feeds) ->
                if feeds.length > 0
                    @.model.set('feeds', feeds)

    configModule: (e) ->
        action = (data)->

            jsRoutes.controllers.Modules.saveJS().ajax
                context: @
                data:
                    data
                success: ->
                    log 'SUCCESS'
                    url = data['url']
                    title = data['title']
                    @.model.set( {'title': title, 'url': url} )
                    return true
                error: (err) ->
                    log 'ERROR'
                    log err

        view = new ModalView({ mName: 'modal/module', model: @.model, action: action})
        view.show()

    markAsRead: (e) ->
        e.currentTarget.style.color = '#999'
        feedId = $(e.currentTarget).data('feed-id')
        log "feedId: #{feedId}"
        $.get('/json/feeds/' + feedId + '/read')

    delete: ->
        if(confirm('Sure to delete ' + @.model.get('title') + ' - ' + @.model.get('url') + ' ?'))
            log "delete: " + @.model.id
            jsRoutes.controllers.Modules.delete(document.tabId, @.model.id).ajax
                context: @
                success: ->
                    log 'success'
            @.remove()


class TabView extends Backbone.View

    template: MUSTACHE_TEMPLATES['tabs/show']

    tagName: "li"

    events:
      'click': 'display',

    render: ->
        log 'render tab'
        @$el.html Mustache.render(@template, @model.toJSON())
        this

    display: (e) ->
        $('#tabs-list li').removeClass('active')
        $(e.currentTarget).addClass("active")
        tabId = e.currentTarget.children[0].dataset['id']
        @._display(tabId)
    
    display2: (id) ->
        $('#tabs-list li').removeClass('active')
        $("#tabs-list li a[data-id=#{id}]").parent().addClass('active')
        @._display(id)
    
    _display: (id) ->
        document.tabId = id
        module = new ModuleList([], {tabId: id})
        module.fetch()
        

class ModalView extends Backbone.View

    events:
        'click .close'      : 'close'
        'submit form'       : 'submit'

    initialize: ->
        this.template = _.template($('#modal').html())

    render: ->
        mName = @.options['mName']
        @$el.html Mustache.render(MUSTACHE_TEMPLATES[mName], @.model.toJSON())
        return @

    submit: (e) ->
        if @.options['action'] != undefined
            e.preventDefault
            try
                inputs = $('input')
                params = {}
                for input in inputs
                    name = input.getAttribute('name')
                    value= input.value
                    params[name] = value

                @.options['action'](params)
                @.close()

            catch error
                log "error: #{error}"

            return false

    show: ->
       $(document.body).append(this.render().el)

    close: ->
        this.remove()

$ ->
    # ------------------------------------------------ MODAL
    # Create TAB
    $('#show-modal').click (e) ->
        action = (data)->
            jsRoutes.controllers.Tabs.save(document.username).ajax
                context: @
                data: data
                success: (result) ->
                    tabs.addOne(new Tab(result))
                error: (err) ->
                    log 'ERROR'
                    log err

        model = new Backbone.Model
        view = new ModalView({ mName: 'modal/createTab', model: model, action : action})
        view.show()

    $('#show-import-feeds').click (e) ->
        model = new Backbone.Model({ username: document.username })
        view = new ModalView({ mName: 'modal/importFeeds', model: model})
        view.show()

    $('#show-new-module').click (e) ->
        e.preventDefault
        action = (data)->
            jsRoutes.controllers.Modules.findRSS(document.tabId).ajax
                context: @
                data: data
                success: (result) ->
                    action = (data) ->
                        jsRoutes.controllers.Modules.create(document.tabId).ajax
                            context: @
                            data: data
                        
                    model = new Backbone.Model({urls: result})
                    view = new ModalView({mName: 'modal/validModule', model: model, action: action})
                    view.show()
                error: (err) ->
                    log 'ERROR'
                    log err

        model = new Module()
        view = new ModalView({mName: 'modal/newModule', model: model, action: action})
        view.show()

tabs = new TabList()
tabs.fetch
    success: ->
        log "fetch success"
        window.App = new AppRouter
        Backbone.history.start
            pushHistory: true


