export const notFound = (_request, response) => {
  response.status(404).json({ message: '接口不存在' });
};

export const errorHandler = (error, _request, response, _next) => {
  console.error(error);
  response.status(error.statusCode || 500).json({ message: error.message || '服务器暂时不可用' });
};

