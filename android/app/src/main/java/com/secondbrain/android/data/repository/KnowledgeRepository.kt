package com.secondbrain.android.data.repository

import com.secondbrain.android.data.remote.CreateKnowledgeRequest
import com.secondbrain.android.data.remote.KnowledgeNode
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import javax.inject.Inject
import javax.inject.Singleton

/** Converts the server envelope to explicit success/failure results for feature ViewModels. */
@Singleton
class KnowledgeRepository @Inject constructor(private val api: SecondBrainApi) {
    suspend fun page(page: Int, keyword: String?) = api.knowledge(page = page, keyword = keyword).requireData()

    suspend fun detail(id: Long): KnowledgeNode = api.knowledgeDetail(id).requireData()

    suspend fun load(keyword: String? = null): Result<List<KnowledgeNode>> = runCatching {
        api.knowledge(keyword = keyword).requireData().records
    }

    suspend fun create(request: CreateKnowledgeRequest): Result<KnowledgeNode> = runCatching {
        api.createKnowledge(request).requireData()
    }
}
