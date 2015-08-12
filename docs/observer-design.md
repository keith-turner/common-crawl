
Observer Design Doc
===================

Below are available operations:

    get(row, col) -> value
    set(row, col, value)
    del(row, col)

PageLoader is called with `pageUri` & `pageJson`

    curJson = get(pageUri, page:cur)
    if curJson != pageJson
      set(pageUri, page:new, pageJson)

PageObserver watches `page:new` is called with `pageUri`

    curJson = get(pageUri, page:cur)
    newJson = get(pageUri, page:new)

    newLinks,delLinks = compare(curJson, newJson)

    for link in newLinks:
      set(linkUri, update:inlinks, Update[add,pageUri,anchorText])

    for link in delLinks
      set(linkUri, update:inlinks, Update[del,pageUri,anchorText])

    if curJson == null:
      pageScore = get(pageUri, stats:pagescore).toInteger(0)

      if pageScore != 0:
        del(pageDomain, ranked:pageScore:pageUri)
      
      set(pageUri, stats:pagescore, pageScore+1)
      set(pageDomain, ranked:pageScore+1:pageUri)

    set(pageUri, page:cur, newJson)
    del(pageUri, page:new)

InlinksObserver weakly watches `update:inlinks` is called with `pageUri`

    List<Update> updates = get(pageUri, update:inlinks)

    change = 0

    for update in updates:
      if update.action == del:
        change--
        del(pageUri, inlinks:update.linkUri)
      elif update.action == add:
        change++
        set(pageUri, inlinks:update.linkUri, update.anchorText)

    if change != 0:
      curCount = get(pageUri, stats:inlinkcount)
      curScore = get(pageUri, stats:pagescore).toInteger(0)

      if pageScore != 0:
        del(pageDomain, ranked:curScore:pageUri)

      set(pageDomain, ranked:curScore+change:pageUri)
      set(pageUri, stats:inlinkcount, curCount+change)
      set(pageUri, stats:pagescore, curScore+change)
